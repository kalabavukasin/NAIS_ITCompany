using Gateway.Configuration;
using Gateway.Models;
using Gateway.Services;
using Microsoft.OpenApi.Models;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();

// Configure Swagger with JWT support
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo 
    { 
        Title = "IT Company Gateway API", 
        Version = "v1",
        Description = "API Gateway za IT Company recruitment sistem"
    });

    // Add JWT Authentication to Swagger
    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Description = "JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"",
        Name = "Authorization",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.ApiKey,
        Scheme = "Bearer"
    });

    c.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            new string[] {}
        }
    });
});

// Configure JWT Authentication
builder.Services.ConfigureJwtAuthentication();

// Configure CORS
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", builder =>
    {
        builder.AllowAnyOrigin()
               .AllowAnyMethod()
               .AllowAnyHeader();
    });
});

// Configure service endpoints
var serviceEndpoints = new ServiceEndpoints
{
    RecruitmentService = Environment.GetEnvironmentVariable("RECRUITMENT_SERVICE_URL") ?? "http://localhost:5001"
};

builder.Services.AddSingleton(serviceEndpoints);
builder.Services.AddHttpClient<IServiceProxy, ServiceProxy>();
builder.Services.AddScoped<IServiceProxy, ServiceProxy>();

var app = builder.Build();

// Configure the HTTP request pipeline
if (app.Environment.IsDevelopment() ||
    string.Equals(app.Environment.EnvironmentName, "Docker", StringComparison.OrdinalIgnoreCase))
{
    app.UseDeveloperExceptionPage();
    app.UseSwagger();
    app.UseSwaggerUI(c =>
    {
        c.SwaggerEndpoint("/swagger/v1/swagger.json", "IT Company Gateway API v1");
        c.RoutePrefix = "swagger";
    });
}

// U kontejneru slušaš HTTP (port 80), zato ne forsiraj HTTPS redirect
if (!string.Equals(app.Environment.EnvironmentName, "Docker", StringComparison.OrdinalIgnoreCase))
{
    app.UseHttpsRedirection();
}

app.UseRouting();
app.UseCors("AllowAll");

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

// Health check endpoint
app.MapGet("/healthz", () => Results.Ok(new { status = "ok", timestamp = DateTime.UtcNow }));

// Root endpoint with API information
app.MapGet("/", () => Results.Ok(new 
{ 
    name = "IT Company Gateway",
    version = "1.0.0",
    description = "API Gateway za IT Company recruitment sistem",
    endpoints = new
    {
        swagger = "/swagger",
        health = "/healthz",
        recruitment = "/api/gateway/recruitment/*"
    }
}));

app.Run();

namespace Gateway
{
    public partial class Program { }
}

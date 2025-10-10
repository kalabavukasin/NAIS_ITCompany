using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using System.Text;

namespace Gateway.Configuration;

public static class JwtConfiguration
{
    public static IServiceCollection ConfigureJwtAuthentication(this IServiceCollection services)
    {
        var key = Environment.GetEnvironmentVariable("JWT_KEY") ?? "explorer_secret_key_very_long_and_secure_key_for_production";
        var issuer = Environment.GetEnvironmentVariable("JWT_ISSUER") ?? "travel-agency";
        var audience = Environment.GetEnvironmentVariable("JWT_AUDIENCE") ?? "travel-agency-users";

        services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer(options =>
            {
                options.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuer = true,
                    ValidateAudience = true,
                    ValidateIssuerSigningKey = true,
                    ValidateLifetime = true,
                    ValidIssuer = issuer,
                    ValidAudience = audience,
                    IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(key))
                };

                options.Events = new JwtBearerEvents
                {
                    OnAuthenticationFailed = context =>
                    {
                        if (context.Exception.GetType() == typeof(SecurityTokenExpiredException))
                        {
                            context.Response.Headers.Append("AuthenticationTokens-Expired", "true");
                        }
                        return Task.CompletedTask;
                    }
                };
            });

        services.AddAuthorization(options =>
        {
            options.AddPolicy("administratorPolicy", policy => policy.RequireRole("Administrator"));
            options.AddPolicy("authorPolicy", policy => policy.RequireRole("Author"));
            options.AddPolicy("touristPolicy", policy => policy.RequireRole("Tourist"));
        });

        return services;
    }
}
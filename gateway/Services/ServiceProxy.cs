using Gateway.Models;

namespace Gateway.Services;

public interface IServiceProxy
{
    Task<HttpResponseMessage> ForwardRequestAsync(string serviceName, string path, HttpMethod method, HttpContent? content = null, string? authToken = null, IDictionary<string, string>? extraHeaders = null);
}

public class ServiceProxy : IServiceProxy
{
    private readonly HttpClient _httpClient;
    private readonly ServiceEndpoints _serviceEndpoints;
    private readonly ILogger<ServiceProxy> _logger;

    public ServiceProxy(HttpClient httpClient, ServiceEndpoints serviceEndpoints, ILogger<ServiceProxy> logger)
    {
        _httpClient = httpClient;
        _serviceEndpoints = serviceEndpoints;
        _logger = logger;
    }

    public async Task<HttpResponseMessage> ForwardRequestAsync(string serviceName, string path, HttpMethod method, HttpContent? content = null, string? authToken = null, IDictionary<string, string>? extraHeaders = null)
    {
        var baseUrl = GetServiceUrl(serviceName);
        if (string.IsNullOrEmpty(baseUrl))
        {
            throw new ArgumentException($"Unknown service: {serviceName}");
        }

        var requestUri = $"{baseUrl.TrimEnd('/')}/{path.TrimStart('/')}";
        
        _logger.LogInformation("Forwarding {Method} request to {Uri}", method, requestUri);

        using var request = new HttpRequestMessage(method, requestUri);
        
        if (content != null)
        {
            request.Content = content;
        }

        if (!string.IsNullOrEmpty(authToken))
        {
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", authToken);
        }

        if (extraHeaders != null)
        {
            foreach (var kvp in extraHeaders)
            {
                request.Headers.TryAddWithoutValidation(kvp.Key, kvp.Value);
            }
        }

        try
        {
            var response = await _httpClient.SendAsync(request);
            _logger.LogInformation("Received {StatusCode} response from {Uri}", response.StatusCode, requestUri);
            return response;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error forwarding request to {Uri}", requestUri);
            throw;
        }
    }

    private string GetServiceUrl(string serviceName)
    {
        return serviceName.ToLower() switch
        {
            "recruitment" => _serviceEndpoints.RecruitmentService,
            _ => string.Empty
        };
    }
}
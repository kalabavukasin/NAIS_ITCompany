using Gateway.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Text;
using System.Net.Http.Headers;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace Gateway.Controllers;

[ApiController]
[Route("api/[controller]")]
public class GatewayController : ControllerBase
{
    private readonly IServiceProxy _serviceProxy;
    private readonly ILogger<GatewayController> _logger;

    public GatewayController(IServiceProxy serviceProxy, ILogger<GatewayController> logger)
    {
        _serviceProxy = serviceProxy;
        _logger = logger;
    }

    // Recruitment service endpoints - Job Postings
    [HttpPost("recruitment/job-postings")]
    public async Task<IActionResult> CreateJobPosting()
    {
        return await ForwardRequest("recruitment", "api/job-postings", HttpMethod.Post);
    }

    [HttpGet("recruitment/job-postings")]
    public async Task<IActionResult> GetAllJobPostings()
    {
        return await ForwardRequest("recruitment", "api/job-postings", HttpMethod.Get);
    }

    [HttpGet("recruitment/job-postings/{id}")]
    public async Task<IActionResult> GetJobPosting(string id)
    {
        return await ForwardRequest("recruitment", $"api/job-postings/{id}", HttpMethod.Get);
    }

    [HttpPut("recruitment/job-postings/{id}")]
    public async Task<IActionResult> UpdateJobPosting(string id)
    {
        return await ForwardRequest("recruitment", $"api/job-postings/{id}", HttpMethod.Put);
    }

    [HttpDelete("recruitment/job-postings/{id}")]
    public async Task<IActionResult> DeleteJobPosting(string id)
    {
        return await ForwardRequest("recruitment", $"api/job-postings/{id}", HttpMethod.Delete);
    }

    // Recruitment service endpoints - Candidates
    [HttpPost("recruitment/candidates")]
    public async Task<IActionResult> CreateCandidate()
    {
        return await ForwardRequest("recruitment", "api/candidates", HttpMethod.Post);
    }

    [HttpGet("recruitment/candidates")]
    public async Task<IActionResult> GetAllCandidates()
    {
        return await ForwardRequest("recruitment", "api/candidates", HttpMethod.Get);
    }

    [HttpGet("recruitment/candidates/{id}")]
    public async Task<IActionResult> GetCandidate(string id)
    {
        return await ForwardRequest("recruitment", $"api/candidates/{id}", HttpMethod.Get);
    }

    [HttpPut("recruitment/candidates/{id}")]
    public async Task<IActionResult> UpdateCandidate(string id)
    {
        return await ForwardRequest("recruitment", $"api/candidates/{id}", HttpMethod.Put);
    }

    [HttpDelete("recruitment/candidates/{id}")]
    public async Task<IActionResult> DeleteCandidate(string id)
    {
        return await ForwardRequest("recruitment", $"api/candidates/{id}", HttpMethod.Delete);
    }

    // Recruitment service endpoints - Applications
    [HttpPost("recruitment/applications")]
    public async Task<IActionResult> CreateApplication()
    {
        return await ForwardRequest("recruitment", "api/applications", HttpMethod.Post);
    }
    
    [HttpPost("recruitment/applications/create")]
    public async Task<IActionResult> CreateApplicationDirect()
    {
        return await ForwardRequest("recruitment", "api/applications/create", HttpMethod.Post);
    }

    [HttpGet("recruitment/applications")]
    public async Task<IActionResult> GetAllApplications()
    {
        return await ForwardRequest("recruitment", "api/applications", HttpMethod.Get);
    }

    [HttpGet("recruitment/applications/{id}")]
    public async Task<IActionResult> GetApplication(string id)
    {
        return await ForwardRequest("recruitment", $"api/applications/{id}", HttpMethod.Get);
    }

    [HttpPut("recruitment/applications/{id}")]
    public async Task<IActionResult> UpdateApplication(string id)
    {
        return await ForwardRequest("recruitment", $"api/applications/{id}", HttpMethod.Put);
    }
    
    [HttpDelete("recruitment/applications/{id}")]
    public async Task<IActionResult> DeleteApplication(string id)
    {
        return await ForwardRequest("recruitment", $"api/applications/{id}", HttpMethod.Delete);
    }

    // Recruitment service endpoints - Search and Analytics
    [HttpPost("recruitment/candidates/search")]
    public async Task<IActionResult> SearchCandidates()
    {
        return await ForwardRequest("recruitment", "api/candidates/search", HttpMethod.Post);
    }

    [HttpPost("recruitment/search/jobs")]
    public async Task<IActionResult> SearchJobs()
    {
        return await ForwardRequest("recruitment", "api/search/jobs", HttpMethod.Post);
    }

   /* [HttpPost("recruitment/vector-search/candidates")]
    public async Task<IActionResult> VectorSearchCandidates()
    {
        return await ForwardRequest("recruitment", "api/candidates/hybrid-search", HttpMethod.Post);
    }*/

    // Complex search endpoints for Elasticsearch testing
    [HttpPost("recruitment/candidates/hybrid-search")]
    public async Task<IActionResult> HybridSearchCandidates()
    {
        return await ForwardRequest("recruitment", "api/candidates/hybrid-search", HttpMethod.Post);
    }

    [HttpPost("recruitment/candidates/search-with-vector")]
    public async Task<IActionResult> SearchCandidatesWithVector()
    {
        return await ForwardRequest("recruitment", "api/candidates/search-with-vector", HttpMethod.Post);
    }

    [HttpPost("recruitment/candidates/hybrid-search-advanced")]
    public async Task<IActionResult> HybridSearchCandidatesAdvanced()
    {
        return await ForwardRequest("recruitment", "api/candidates/hybrid-search-advanced", HttpMethod.Post);
    }

    [HttpGet("recruitment/candidates/count-by-location/{location}")]
    public async Task<IActionResult> CountCandidatesByLocation(string location)
    {
        return await ForwardRequest("recruitment", $"api/candidates/count-by-location/{location}", HttpMethod.Get);
    }

    [HttpPost("recruitment/candidates/single-vector-search")]
    public async Task<IActionResult> SingleVectorSearchCandidates()
    {
        return await ForwardRequest("recruitment", "api/candidates/single-vector-search", HttpMethod.Post);
    }

    [HttpPost("recruitment/candidates/search-candidates")]
    public async Task<IActionResult> SearchCandidatesBasic()
    {
        return await ForwardRequest("recruitment", "api/candidates/search-candidates", HttpMethod.Post);
    }

    [HttpGet("recruitment/candidates/rank/{jobPostingId}")]
    public async Task<IActionResult> RankCandidates(string jobPostingId)
    {
        return await ForwardRequest("recruitment", $"api/candidates/rank/{jobPostingId}", HttpMethod.Get);
    }

    [HttpPost("recruitment/jobs/search")]
    public async Task<IActionResult> SearchJobsWithFilters()
    {
        return await ForwardRequest("recruitment", "api/jobs/search", HttpMethod.Post);
    }


    [HttpGet("recruitment/analytics/statistics")]
    public async Task<IActionResult> GetRecruitmentStatistics()
    {
        return await ForwardRequest("recruitment", "api/analytics/statistics", HttpMethod.Get);
    }

    private async Task<IActionResult> ForwardRequest(string serviceName, string path, HttpMethod method, bool includeAuth = false)
    {
        try
        {
            HttpContent? content = null;
            
            if (method == HttpMethod.Post || method == HttpMethod.Put || method == HttpMethod.Patch)
            {
                Request.EnableBuffering();
                Request.Body.Position = 0;
                var body = await ReadRequestBodyAsync();
                if (!string.IsNullOrEmpty(body))
                {
                    content = new StringContent(body, Encoding.UTF8, "application/json");
                }
            }

            // Add query parameters to path
            if (Request.QueryString.HasValue)
            {
                path += Request.QueryString.Value;
            }

            string? authToken = null;
            if (includeAuth)
            {
                authToken = ExtractTokenFromHeader();
            }

            var response = await _serviceProxy.ForwardRequestAsync(serviceName, path, method, content, authToken);
            
            var responseContent = await response.Content.ReadAsStringAsync();
            
            return StatusCode((int)response.StatusCode, 
                string.IsNullOrEmpty(responseContent) ? null : responseContent);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing request for {ServiceName}/{Path}", serviceName, path);
            return StatusCode(500, new { message = "Gateway error occurred" });
        }
    }


    private async Task<string> ReadRequestBodyAsync()
    {
        using var reader = new StreamReader(Request.Body, leaveOpen: true);
        return await reader.ReadToEndAsync();
    }

    private string? ExtractTokenFromHeader()
    {
        var authHeader = Request.Headers.Authorization.FirstOrDefault();
        if (authHeader != null && authHeader.StartsWith("Bearer "))
        {
            return authHeader.Substring("Bearer ".Length);
        }
        return null;
    }
}
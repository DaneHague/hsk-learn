using HskLearn.Api.Endpoints;
using HskLearn.Api.Services;
using Scalar.AspNetCore;

var builder = WebApplication.CreateBuilder(args);

// Bind configuration sections
builder.Services.Configure<AzureSpeechOptions>(builder.Configuration.GetSection("AzureSpeech"));
builder.Services.Configure<AzureAIFoundryOptions>(builder.Configuration.GetSection("AzureAIFoundry"));

// Register services
builder.Services.AddSingleton<VocabularyService>();
builder.Services.AddSingleton<ReadingTaskService>();
builder.Services.AddSingleton<SpeechAssessmentService>();
builder.Services.AddSingleton<WritingEvaluationService>();
builder.Services.AddSingleton<ProgressService>();

// HttpClient for Azure AI Foundry
builder.Services.AddHttpClient();

// OpenAPI
builder.Services.AddOpenApi();

// Request timeout
builder.Services.AddRequestTimeouts(options =>
{
    options.DefaultPolicy = new Microsoft.AspNetCore.Http.Timeouts.RequestTimeoutPolicy
    {
        Timeout = TimeSpan.FromSeconds(60),
    };
});

// CORS — allow all origins for dev (Android device/emulator network requests)
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
        policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader());
});

// ProblemDetails for consistent error responses
builder.Services.AddProblemDetails();

var app = builder.Build();

// Global exception handler → ProblemDetails JSON
app.UseExceptionHandler();
app.UseStatusCodePages();

app.UseCors();
app.UseRequestTimeouts();

// OpenAPI + Scalar docs
app.MapOpenApi();
app.MapScalarApiReference();

// Health check
app.MapGet("/health", (VocabularyService vocab) =>
{
    return TypedResults.Ok(new
    {
        status = "healthy",
        vocabCount = vocab.GetTotalCount(),
        timestamp = DateTime.UtcNow,
    });
}).WithTags("Health").WithName("HealthCheck");

// Map all endpoint groups under /api/v1
var api = app.MapGroup("/api/v1");
api.MapVocabularyEndpoints();
api.MapReadingEndpoints();
api.MapSpeechEndpoints();
api.MapWritingEndpoints();
api.MapProgressEndpoints();

app.Run();

// Strongly-typed options
public class AzureSpeechOptions
{
    public string SubscriptionKey { get; set; } = string.Empty;
    public string Region { get; set; } = string.Empty;
}

public class AzureAIFoundryOptions
{
    public string Endpoint { get; set; } = string.Empty;
    public string ApiKey { get; set; } = string.Empty;
    public string DeploymentName { get; set; } = string.Empty;
}

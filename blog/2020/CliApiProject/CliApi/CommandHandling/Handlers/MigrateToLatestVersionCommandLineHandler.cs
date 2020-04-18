namespace CliApi.CommandHandling.Handlers
{
    using System.CommandLine.Invocation;
    using System.CommandLine.IO;
    using System.Threading.Tasks;
    using Dependencies;
    using Microsoft.Extensions.Logging;

    public class MigrateToLatestVersionCommandHandler : ICommandHandler
    {
        private readonly IMigrationStrategy _migrationStrategy;
        private readonly ILogger<MigrateToLatestVersionCommandHandler> _logger;

        public MigrateToLatestVersionCommandHandler(
            IMigrationStrategy migrationStrategy,
            ILogger<MigrateToLatestVersionCommandHandler> logger)
        {
            _migrationStrategy = migrationStrategy;
            _logger = logger;
        }

        public Task<int> InvokeAsync(InvocationContext context)
        {
            string strategy = _migrationStrategy.Get();
            context.Console.Out.WriteLine($"The selected migration strategy:{strategy}");
            _logger.LogInformation("Migration Strategy: {strategy}", strategy);
            context.Console.Out.Write($"{nameof(MigrateToLatestVersionCommandHandler)} was executed successfully!");
            
            return Task.FromResult(ExitCode.Ok);
        }
    }
}
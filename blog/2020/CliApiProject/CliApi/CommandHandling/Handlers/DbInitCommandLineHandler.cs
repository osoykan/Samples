namespace CliApi.CommandHandling.Handlers
{
    using System.CommandLine.Invocation;
    using System.Threading.Tasks;

    public class DbInitCommandLineHandler : ICommandHandler
    {
        public Task<int> InvokeAsync(InvocationContext context)
        {
            context.Console.Out.Write($"{nameof(DbInitCommandLineHandler)} was executed successfully!");
            return Task.FromResult(ExitCode.Ok);
        }
    }
}
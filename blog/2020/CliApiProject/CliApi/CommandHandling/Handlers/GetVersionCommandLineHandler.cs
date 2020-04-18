namespace CliApi.CommandHandling.Handlers
{
    using System.CommandLine.Invocation;
    using System.Threading.Tasks;

    public class GetVersionCommandLineHandler: ICommandHandler
    {
        public Task<int> InvokeAsync(InvocationContext context)
        {
            context.Console.Out.Write(GetType().Assembly.ImageRuntimeVersion);
            return Task.FromResult(ExitCode.Ok);
        }
    }
}
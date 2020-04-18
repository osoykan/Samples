namespace CliApi
{
    using System.CommandLine.Parsing;
    using System.Threading;
    using System.Threading.Tasks;
    using CommandHandling;
    using Microsoft.Extensions.Hosting;

    public static class Program
    {
        public static async Task<int> Main(string[] args)
        {
            var factory = new ApplicationFactory();
            int exitCode = await factory
                .CreateCommandLineBuilder(args)
                .Build()
                .InvokeAsync(args);

            if (exitCode != ExitCode.BranchToWeb)
                return exitCode;

            await CreateHostBuilder(args).Build().RunAsync(CancellationToken.None);
            return ExitCode.Ok;
        }

        /// <summary>
        ///  We keep this builder to test the Application's Web Entry from the proper place
        ///  where we actually use with normal flow
        /// </summary>
        /// <param name="args"></param>
        /// <returns></returns>
        private static IHostBuilder CreateHostBuilder(string[] args)
        {
            var factory = new ApplicationFactory();
            return factory.CreateWebHostBuilder(args);
        }
    }
}
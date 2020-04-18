namespace CliApi.Tests
{
    using System.Collections.Generic;
    using System.CommandLine;
    using System.CommandLine.Builder;
    using System.CommandLine.IO;
    using System.CommandLine.Parsing;
    using System.Linq;
    using System.Threading.Tasks;
    using Microsoft.Extensions.Configuration;
    using Microsoft.Extensions.DependencyInjection;
    using Xunit.Abstractions;

    public abstract class CliTestBase : ApplicationFactory
    {
        private readonly ITestOutputHelper _testOutputHelper;

        protected CliTestBase(ITestOutputHelper testOutputHelper)
        {
            _testOutputHelper = testOutputHelper;
        }
        
        protected TestConsole Console { get; } = new TestConsole();
        protected string ConsoleStdOut => Console.Out.ToString();
        protected string ConsoleErrorOut => Console.Error.ToString();
        protected Dictionary<string, string> Configuration { get; set; } = new Dictionary<string, string>();

        public override CommandLineBuilder CreateCommandLineBuilder(string[] args) 
        {
            CommandLineBuilder cliBuilder = base.CreateCommandLineBuilder(args);
            ConfigureServices += services =>
            {
                services.AddSingleton<IConsole>(Console);
            };
            ConfigureAppConfiguration += builder =>
            {
                if (Configuration != null && Configuration.Any()) builder.AddInMemoryCollection(Configuration);
            };
            return cliBuilder.ConfigureConsole(context => Console);
        }
        
        protected async Task<int> Execute(params string[] args)
        {
            int returnValue = await CreateCommandLineBuilder(args).Build().InvokeAsync(args);
            if (!string.IsNullOrEmpty(ConsoleStdOut))
            {
                _testOutputHelper.WriteLine("-----CLI STD OUT-----");
                _testOutputHelper.WriteLine($"{ConsoleStdOut}");
            }

            if (!string.IsNullOrEmpty(ConsoleErrorOut))
            {
                _testOutputHelper.WriteLine("-----CLI ERROR-----");
                _testOutputHelper.WriteLine($"{ConsoleErrorOut}");
            }

            return returnValue;
        }
    }
}
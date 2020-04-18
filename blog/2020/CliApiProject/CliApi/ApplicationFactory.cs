namespace CliApi
{
    using System;
    using System.CommandLine;
    using System.CommandLine.Builder;
    using System.CommandLine.Hosting;
    using System.CommandLine.IO;
    using CommandHandling;
    using CommandHandling.Handlers.Dependencies;
    using Microsoft.AspNetCore.Hosting;
    using Microsoft.Extensions.Configuration;
    using Microsoft.Extensions.DependencyInjection;
    using Microsoft.Extensions.Hosting;

    /// <summary>
    /// Responsible with creation of Application's building blocks
    /// WebHost and Cli Host
    /// </summary>
    public class ApplicationFactory
    {
        /// <summary>
        ///     Provides an extensibility to register dependencies for CLI Integration or Unit testing
        /// </summary>
        protected Action<IServiceCollection> ConfigureServices { get; set; } = services => { };

        /// <summary>
        ///     Provides an extensibility to <see cref="IConfiguration" /> for CLI Integration or Unit testing
        /// </summary>
        protected Action<IConfigurationBuilder> ConfigureAppConfiguration { get; set; } = config => { };

        /// <summary>
        ///     .NetCore Web API entrance for Main.
        ///     Creates the HostBuilder to be executed in Main.
        /// </summary>
        /// <param name="args"></param>
        /// <returns></returns>
        public virtual IHostBuilder CreateWebHostBuilder(string[] args)
        {
            return Host.CreateDefaultBuilder(args)
                .ConfigureWebHostDefaults(webBuilder => { webBuilder.UseStartup<Startup>(); });
        }

        /// <summary>
        ///     Builds the command-line pipeline
        /// </summary>
        /// <returns></returns>
        public virtual CommandLineBuilder CreateCommandLineBuilder(string[] args)
        {
            return new CommandLineBuilder(
                    new RootCommand().CreateCommandHandling()
                )
                .UseHost(host =>
                    host.ConfigureServices(services =>
                        {
                            services.AddSingleton<IConsole, SystemConsole>();
                            services.AddTransient<IMigrationStrategy, CloudMigrationStrategy>();
                            services.AddCommandLineHandlers();
                            ConfigureServices(services);
                        })
                        .ConfigureAppConfiguration(builder =>
                        {
                            builder.AddCommandLine(args);
                            ConfigureAppConfiguration(builder);
                        })
                )
                .BranchToWebIfNoCommandToExecute();
        }
    }
}
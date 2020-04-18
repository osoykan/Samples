namespace CliApi.CommandHandling
{
    using System;
    using System.Collections.Generic;
    using System.CommandLine;
    using System.CommandLine.Builder;
    using System.CommandLine.Invocation;
    using System.CommandLine.IO;
    using System.CommandLine.Parsing;
    using System.Linq;
    using System.Threading.Tasks;
    using Handlers;
    using Microsoft.Extensions.DependencyInjection;
    using Microsoft.Extensions.Hosting;
    using Microsoft.Extensions.Logging;

    /// <summary>
    /// Command handling extensions
    /// </summary>
    public static class CommandExtensions
    {
        /// <summary>
        /// Makes easier the addressing handlers to the commands and also enables dependency injection as default.
        /// </summary>
        /// <param name="command"></param>
        /// <typeparam name="T"></typeparam>
        /// <returns></returns>
        public static Command HandledBy<T>(this Command command) where T : ICommandHandler
        {
            command.Handler = CommandHandler.Create<IHost, InvocationContext>((host, invocation) =>
            {
                var logger = host.Services.GetService<ILogger<T>>();
                if (command is ValidatedCommand toBeValidated)
                {
                    (bool isValid, string reason) = toBeValidated.Validate(host.Services);
                    if (!isValid)
                    {
                        invocation.Console.Error.WriteLine(reason);
                        logger.LogError(reason);
                        invocation.ResultCode = (int) ExitCode.Error;
                        return Task.FromResult(ExitCode.Error);
                    }
                }

                if (invocation.HasErrors())
                {
                    invocation.PrintErrors();
                    return Task.FromResult(ExitCode.Error);
                }

                return host.Services.GetRequiredService<T>().InvokeAsync(invocation);
            });

            return command;
        }

        /// <summary>
        /// Adds the given command and returns the command itself
        /// </summary>
        /// <param name="command"></param>
        /// <param name="subCommand"></param>
        /// <returns></returns>
        public static Command AddSubCommand(this Command command, Command subCommand)
        {
            command.AddCommand(subCommand);
            return command;
        }

        /// <summary>
        /// Configures the command and returns the instance.
        /// </summary>
        /// <param name="command"></param>
        /// <param name="configure"></param>
        /// <returns></returns>
        public static Command Configure(this Command command, Action<Command> configure)
        {
            configure(command);
            return command;
        }

        /// <summary>
        /// Configures the command and returns the instance.
        /// </summary>
        /// <param name="command"></param>
        /// <param name="configure"></param>
        /// <returns></returns>
        public static Command Configure(this ValidatedCommand command, Action<ValidatedCommand> configure)
        {
            configure(command);
            return command;
        }

        /// <summary>
        /// Configures command handling pipeline by adding a middleware, if incoming command is not handled by the pipeline,
        /// in other words if it is not known then the pipeline passes through these args to the application entry point.
        /// </summary>
        /// <param name="builder"></param>
        /// <returns></returns>
        public static CommandLineBuilder BranchToWebIfNoCommandToExecute(this CommandLineBuilder builder)
        {
            bool IsKnownCommand(IEnumerable<Token> tokens) => tokens.All(x => x.Type == TokenType.Command);
            builder.UseMiddleware(async (context, next) =>
            {
                IReadOnlyList<Token> tokens = context.ParseResult.Tokens;
                if (IsKnownCommand(tokens))
                {
                    await next(context);
                }
                else
                {
                    context.ResultCode = ExitCode.BranchToWeb;
                }
            });
            return builder;
        }

        /// <summary>
        /// Adds application's command handlers
        /// </summary>
        /// <param name="services"></param>
        /// <returns></returns>
        public static IServiceCollection AddCommandLineHandlers(this IServiceCollection services)
        {
            services.AddTransient<DbInitCommandLineHandler>();
            services.AddTransient<MigrateToLatestVersionCommandHandler>();
            return services;
        }

        /// <summary>
        /// Creates command line handling and appends handlers to the provided <see cref="RootCommand"/>
        /// </summary>
        /// <param name="root"></param>
        /// <param name="settings"></param>
        /// <returns></returns>
        public static Command CreateCommandHandling(this RootCommand root)
        {
            Command version = new Command("version")
                .Configure(c => { c.AddAlias("--version"); })
                .HandledBy<GetVersionCommandLineHandler>();

            Command db = new Command("db")
                .Configure(c => { c.AddAlias("--db"); })
                .AddSubCommand(new ValidatedCommand("init")
                    .Configure(c =>
                    {
                        c.AddAlias("--init");
                        c.AddAlias("-i");
                        c.AddValidation(sp => { return (true, string.Empty); });
                    })
                    .HandledBy<DbInitCommandLineHandler>())
                .AddSubCommand(new ValidatedCommand("migrate")
                    .Configure(c =>
                    {
                        c.AddAlias("--migrate");
                        c.AddAlias("-m");
                        c.AddValidation(sp => { return (true, string.Empty); });
                    })
                    .HandledBy<MigrateToLatestVersionCommandHandler>());

            Command rootCommand = root
                .AddSubCommand(db)
                .AddSubCommand(version);

            return rootCommand;
        }

        /// <summary>
        /// Prints parse errors to the console
        /// </summary>
        /// <param name="context"></param>
        public static void PrintErrors(this InvocationContext context)
        {
            if (!context.HasErrors())
            {
                return;
            }

            foreach (ParseError error in context.ParseResult.Errors)
            {
                context.Console.Error.WriteLine(error.Message);
            }
        }

        /// <summary>
        /// Returns true if there is any parsing error
        /// </summary>
        /// <param name="context"></param>
        public static bool HasErrors(this InvocationContext context)
        {
            return context.ParseResult.Errors.Any();
        }
    }
}
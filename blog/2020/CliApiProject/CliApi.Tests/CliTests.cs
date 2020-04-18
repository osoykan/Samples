namespace CliApi.Tests
{
    using System.Threading.Tasks;
    using Xunit;
    using Xunit.Abstractions;
    using CommandHandling.Handlers;
    using CommandHandling.Handlers.Dependencies;
    using FakeItEasy;
    using Microsoft.Extensions.DependencyInjection;

    public class CliTests : CliTestBase
    {
        public CliTests(ITestOutputHelper testOutputHelper) : base(testOutputHelper)
        {
        }

        [Fact]
        public async Task Can_Execute_DbInitCommandLineHandler()
        {
            int exitCode = await Execute("db", "init");

            Assert.Equal(0, exitCode);
            Assert.Contains($"{nameof(DbInitCommandLineHandler)} was executed successfully",
                ConsoleStdOut);
        }

        [Fact]
        public async Task Can_Execute_MigrateToLatestVersionCommandLineHandler()
        {
            int exitCode = await Execute("db", "migrate");

            Assert.Equal(0, exitCode);
            Assert.Contains(
                $"{nameof(MigrateToLatestVersionCommandHandler)} was executed successfully",
                ConsoleStdOut);
            Assert.Contains(
                "Cloud",
                ConsoleStdOut);
        }

        [Fact]
        public async Task Can_Execute_MigrationStrategy_With_Something_From_Test_Context()
        {
            const string migrationStrategy = "test";
            var fakeMigrationStrategy = A.Fake<IMigrationStrategy>();
            A.CallTo(() => fakeMigrationStrategy.Get()).Returns(migrationStrategy);
            ConfigureServices += services =>
            {
                services.AddTransient(sp => fakeMigrationStrategy);
            };
            
            int exitCode = await Execute("db", "migrate");

            Assert.Equal(0, exitCode);
            Assert.Contains(
                $"{nameof(MigrateToLatestVersionCommandHandler)} was executed successfully",
                ConsoleStdOut);
            Assert.Contains(
                migrationStrategy,
                ConsoleStdOut);
        }
    }
}
namespace Abp.Quartz.TestConsoleApp
{
    internal class Program
    {
        private static void Main(string[] args)
        {
            var bootstrapper = AbpBootstrapper.Create<AbpQuartzConsoleAppModule>();
            bootstrapper.Initialize();
        }
    }
}
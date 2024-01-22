namespace Abp.Quartz.ConsoleApp
{
    internal class Program
    {
        private static void Main(string[] args)
        {
            var bootstrapper = AbpBootstrapper.Create<AbpQuartzJobExecuterModule>();
            bootstrapper.Initialize();
        }
    }
}
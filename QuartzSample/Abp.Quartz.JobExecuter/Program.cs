namespace Abp.Quartz.JobExecuter
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
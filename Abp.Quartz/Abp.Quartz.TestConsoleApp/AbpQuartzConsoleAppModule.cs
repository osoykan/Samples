using System.Reflection;

using Abp.Modules;
using Abp.Quartz.Configuration;
using Abp.Quartz.Quartz;

using AbpQuartzTask.GoodbyeJob;
using AbpQuartzTask.HelloJob;

namespace Abp.Quartz.TestConsoleApp
{
    [DependsOn(
        typeof(AbpQuartzModule),
        typeof(HelloJobModule),
        typeof(GoodbyeJobModule))]
    public class AbpQuartzConsoleAppModule : AbpModule
    {
        public override void PreInitialize()
        {
            Configuration.BackgroundJobs.UseQuartz();
        }

        public override void Initialize()
        {
            IocManager.RegisterAssemblyByConvention(Assembly.GetExecutingAssembly());
        }
    }
}
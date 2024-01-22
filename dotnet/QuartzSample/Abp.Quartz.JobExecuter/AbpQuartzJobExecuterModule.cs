using System.Reflection;

using Abp.Modules;
using Abp.Quartz.Quartz;
using Abp.Quartz.Quartz.Configuration;

using AbpQuartzTask.GoodbyeJob;
using AbpQuartzTask.HelloJob;

namespace Abp.Quartz.ConsoleApp
{
    [DependsOn(
         typeof(AbpQuartzModule),
         typeof(HelloJobModule),
         typeof(GoodbyeJobModule))]
    public class AbpQuartzJobExecuterModule : AbpModule
    {
        public override void Initialize()
        {
            IocManager.RegisterAssemblyByConvention(Assembly.GetExecutingAssembly());
        }

        public override void PreInitialize()
        {
            Configuration.BackgroundJobs.UseQuartz();
        }
    }
}
using System.Reflection;

using Abp.Modules;
using Abp.Quartz.Configuration;
using Abp.Quartz.Quartz.Configuration;

using Quartz;
using Quartz.Impl;
using Quartz.Spi;

namespace Abp.Quartz.Quartz
{
    [DependsOn(typeof(AbpKernelModule))]
    public class AbpQuartzModule : AbpModule
    {
        public override void Initialize()
        {
            IocManager.RegisterAssemblyByConvention(Assembly.GetExecutingAssembly());
        }

        public override void PostInitialize()
        {
            Configuration.Modules.AbpQuartz().Scheduler.Start();
        }

        public override void PreInitialize()
        {
            IocManager.Register<IAbpQuartzConfiguration, AbpQuartzConfiguration>();

            Configuration.BackgroundJobs.UseQuartz(configuration =>
            {
                configuration.Scheduler = StdSchedulerFactory.GetDefaultScheduler();
                configuration.Scheduler.JobFactory = IocManager.Resolve<IJobFactory>();
                configuration.Scheduler.ListenerManager.AddJobListener(IocManager.Resolve<IJobListener>());
            });
        }
    }
}
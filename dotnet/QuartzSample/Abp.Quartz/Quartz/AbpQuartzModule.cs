using System.Reflection;

using Abp.Modules;
using Abp.Quartz.Configuration;
using Abp.Quartz.Quartz.Configuration;
using Abp.Threading.BackgroundWorkers;

using Quartz;
using Quartz.Spi;

namespace Abp.Quartz.Quartz
{
    [DependsOn(typeof(AbpKernelModule))]
    public class AbpQuartzModule : AbpModule
    {
        public override void Initialize()
        {
            IocManager.RegisterAssemblyByConvention(Assembly.GetExecutingAssembly());

            if (Configuration.BackgroundJobs.IsJobExecutionEnabled)
            {
                var workerManager = IocManager.Resolve<IBackgroundWorkerManager>();
                workerManager.Start();
                workerManager.Add(IocManager.Resolve<IQuartzScheduleJobManager>());
            }
        }

        public override void PostInitialize()
        {
            Configuration.BackgroundJobs.UseQuartz(configuration =>
            {
                configuration.Scheduler.JobFactory = IocManager.Resolve<IJobFactory>();
                configuration.Scheduler.ListenerManager.AddJobListener(IocManager.Resolve<IJobListener>());
            });
        }

        public override void PreInitialize()
        {
            IocManager.Register<IAbpQuartzConfiguration, AbpQuartzConfiguration>();
            IocManager.Register<IQuartzScheduleJobManager, QuartzScheduleJobManager>();
        }
    }
}
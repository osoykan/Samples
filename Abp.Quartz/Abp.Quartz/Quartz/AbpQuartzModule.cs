using System.Reflection;

using Abp.Modules;
using Abp.Quartz.Configuration;
using Abp.Threading.BackgroundWorkers;

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

            Configuration.BackgroundJobs.UseQuartz(configuration =>
            {
                configuration.Scheduler = StdSchedulerFactory.GetDefaultScheduler();
                configuration.Scheduler.JobFactory = IocManager.Resolve<IJobFactory>();
                configuration.Scheduler.ListenerManager.AddJobListener(IocManager.Resolve<IJobListener>());
            });

            IocManager.Register<IQuartzScheduleJobManager, QuartzScheduleJobManager>();

            if (Configuration.BackgroundJobs.IsJobExecutionEnabled)
            {
                var workerManager = IocManager.Resolve<IBackgroundWorkerManager>();
                workerManager.Start();
                workerManager.Add(IocManager.Resolve<IQuartzScheduleJobManager>());
            }
        }
    }
}
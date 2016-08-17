using System.Reflection;

using Abp.Modules;
using Abp.Quartz.Configuration;
using Abp.Quartz.Quartz;
using Abp.Threading.BackgroundWorkers;

using AbpQuartzTask.GoodbyeJob;
using AbpQuartzTask.HelloJob;

namespace Abp.Quartz.TestConsoleApp
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

            //Configuration.BackgroundJobs.UseQuartz();
        }

        public override void PostInitialize()
        {
            if (Configuration.BackgroundJobs.IsJobExecutionEnabled)
            {
                var workerManager = IocManager.Resolve<IBackgroundWorkerManager>();
                workerManager.Start();
                workerManager.Add(IocManager.Resolve<IQuartzScheduleJobManager>());
            }
        }
    }
}
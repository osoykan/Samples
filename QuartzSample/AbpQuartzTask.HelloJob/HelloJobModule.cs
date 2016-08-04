using System.Reflection;

using Abp.Modules;
using Abp.Quartz.Configuration;
using Abp.Quartz.Quartz;

namespace AbpQuartzTask.HelloJob
{
    [DependsOn(typeof(AbpQuartzModule))]
    public class HelloJobModule : AbpModule
    {
        public override void Initialize()
        {
            IocManager.RegisterAssemblyByConvention(Assembly.GetExecutingAssembly());
        }

        public override void PostInitialize()
        {
            var job = IocManager.Resolve<HelloJob>().Build();

            Configuration.Modules.AbpQuartz().Scheduler.ScheduleJob(job.JobDetail, job.Trigger);
        }
    }
}
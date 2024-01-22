using System.Reflection;

using Abp.Dependency;
using Abp.Modules;
using Abp.Quartz.Quartz;

using Quartz;

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
            using (var quartzJobManager = IocManager.ResolveAsDisposable<IQuartzScheduleJobManager>())
            {
                quartzJobManager.Object.ScheduleAsync<HelloJob>(
                                     job =>
                                     {
                                         job.WithIdentity("Hello", "Group1")
                                             .WithDescription("HelloJob");
                                     },
                                     trigger =>
                                     {
                                         trigger.StartNow()
                                                 .WithSimpleSchedule(schedule =>
                                                     schedule.RepeatForever()
                                                              .WithIntervalInSeconds(3)
                                                              .Build());
                                     });
            }
        }
    }
}
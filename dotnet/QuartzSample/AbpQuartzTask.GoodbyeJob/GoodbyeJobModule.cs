using System.Reflection;

using Abp.Dependency;
using Abp.Modules;
using Abp.Quartz.Quartz;

using Quartz;

namespace AbpQuartzTask.GoodbyeJob
{
    [DependsOn(typeof(AbpQuartzModule))]
    public class GoodbyeJobModule : AbpModule
    {
        public override void Initialize()
        {
            IocManager.RegisterAssemblyByConvention(Assembly.GetExecutingAssembly());
        }

        public override void PostInitialize()
        {
            using (var quartzJobManager = IocManager.ResolveAsDisposable<IQuartzScheduleJobManager>())
            {
                quartzJobManager.Object.ScheduleAsync<GoodbyeJob>(
                                     job =>
                                     {
                                         job.WithIdentity("Goodbye", "Group1")
                                             .WithDescription("GoodbyeJob");
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
using Abp.Dependency;
using Abp.Quartz.Quartz;

using Castle.Core.Logging;

using Quartz;

namespace AbpQuartzTask.HelloJob
{
    public class HelloJob : AbpQuartzJobBase, IJob, ITransientDependency
    {
        public ILogger Logger { get; set; }

        public HelloJob()
        {
            Logger = NullLogger.Instance;
        }

        public void Execute(IJobExecutionContext context)
        {
            Logger.Debug($"I'am in the {context.JobDetail.Description} now :)");
        }

        public override void CreateJobDetail()
        {
            base.CreateJobDetail();
            JobBuilder.WithDescription("Hello Job");
        }

        public override void CreateJobTrigger()
        {
            Trigger = TriggerBuilder.Create()
                                    .StartNow()
                                    .WithSimpleSchedule(x => x
                                        .WithIntervalInSeconds(1)
                                        .RepeatForever()
                                        .WithMisfireHandlingInstructionFireNow())
                                    .Build();
        }
    }
}
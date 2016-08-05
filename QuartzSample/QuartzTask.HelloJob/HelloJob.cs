using System;
using System.Threading;

using NLog;

using Quartz;

using QuartzCore.Quartz;

using QuartzTask.HelloJob.Dummy;

namespace QuartzTask.HelloJob
{
    public class HelloJob : JobBase, IJob
    {
        private readonly IDummy dummy;
        private readonly ILogger logger;

        public HelloJob(ILogger logger, IDummy dummy)
        {
            this.logger = logger;
            this.dummy = dummy;
        }

        public void Execute(IJobExecutionContext context)
        {
            logger.Debug($"I'am in the {context.JobDetail.Description} now :)");
            Console.WriteLine($"Hello i am a job running under {Thread.CurrentThread.Name}, and my name is: {context.JobDetail.Description}!..");
            logger.Debug($"Now i'm leaving {context.JobDetail.Description} now :)");
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
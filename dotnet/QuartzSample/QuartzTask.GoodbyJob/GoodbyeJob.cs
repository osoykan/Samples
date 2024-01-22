using System;
using System.Threading;

using NLog;

using Quartz;

using QuartzCore.Quartz;

namespace QuartzTask.GoodbyeJob
{
    public class GoodbyeJob : JobBase, IJob
    {
        private readonly ILogger logger;

        public GoodbyeJob(ILogger logger)
        {
            this.logger = logger;
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
            JobBuilder.WithDescription("Good Bye Job");
        }

        public override void CreateJobTrigger()
        {
            Trigger = TriggerBuilder.Create()
                                    .StartNow()
                                    .WithSimpleSchedule(x => x
                                        .WithIntervalInSeconds(5)
                                        .RepeatForever()
                                        .WithMisfireHandlingInstructionFireNow())
                                    .Build();
        }
    }
}
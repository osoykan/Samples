using NLog;

using Quartz;

namespace QuartzCore.Quartz
{
    public class JobListener : IJobListener
    {
        private readonly ILogger logger;

        public JobListener(ILogger logger)
        {
            this.logger = logger;
        }

        public void JobToBeExecuted(IJobExecutionContext context)
        {
            logger.Info($"Job {context.JobDetail.JobType.Name} executing...");
        }

        public void JobExecutionVetoed(IJobExecutionContext context)
        {
            logger.Info($"Job {context.JobDetail.JobType.Name} executing operation vetoed...");
        }

        public void JobWasExecuted(IJobExecutionContext context, JobExecutionException jobException)
        {
            if (jobException == null)
            {
                logger.Info($"Job {context.JobDetail.JobType.Name} sucessfully executed.");
            }
            else
            {
                logger.Error($"Job {context.JobDetail.JobType.Name} failed with exception:{jobException}");
            }
        }

        public string Name { get; } = "MyJobListener";
    }
}
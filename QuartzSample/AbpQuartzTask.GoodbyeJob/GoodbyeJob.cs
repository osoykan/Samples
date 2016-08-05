using System;

using Abp.Dependency;

using Castle.Core.Logging;

using Quartz;

namespace AbpQuartzTask.GoodbyeJob
{
    public class GoodbyeJob : IJob, ITransientDependency
    {
        public ILogger Logger { get; set; }

        public GoodbyeJob()
        {
            Logger = NullLogger.Instance;
        }

        public void Execute(IJobExecutionContext context)
        {
            Console.WriteLine($"I'am in the {context.JobDetail.Description} now :)");
            Logger.Debug($"I'am in the {context.JobDetail.Description} now :)");
        }
    }
}
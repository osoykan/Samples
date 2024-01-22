using System;
using System.Threading.Tasks;

using Abp.Threading.BackgroundWorkers;

using Quartz;

namespace Abp.Quartz.Quartz
{
    public interface IQuartzScheduleJobManager : IBackgroundWorker
    {
        Task ScheduleAsync<TJob>(Action<JobBuilder> configureJob, Action<TriggerBuilder> configureTrigger) where TJob : IJob;
    }
}
﻿using System;
using System.Threading.Tasks;

using Abp.BackgroundJobs;
using Abp.Quartz.Quartz.Configuration;
using Abp.Threading.BackgroundWorkers;

using Quartz;
using Quartz.Impl;

namespace Abp.Quartz.Quartz
{
    public class QuartzScheduleJobManager : BackgroundWorkerBase, IQuartzScheduleJobManager
    {
        private readonly IAbpQuartzConfiguration quartzConfiguration;
        private readonly IBackgroundJobConfiguration backgroundJobConfiguration;

        public QuartzScheduleJobManager(IAbpQuartzConfiguration quartzConfiguration, IBackgroundJobConfiguration backgroundJobConfiguration)
        {
            this.quartzConfiguration = quartzConfiguration;
            this.backgroundJobConfiguration = backgroundJobConfiguration;
        }

        public override void Start()
        {
            base.Start();

            if (backgroundJobConfiguration.IsJobExecutionEnabled)
            {
                quartzConfiguration.Scheduler.Start();
            }
        }

        public override void WaitToStop()
        {
            if (quartzConfiguration.Scheduler != null)
            {
                try
                {
                    quartzConfiguration.Scheduler.Shutdown(true);
                }
                catch (Exception ex)
                {
                    Logger.Warn(ex.ToString(), ex);
                }
            }

            base.WaitToStop();
        }

        public Task ScheduleAsync<TJob>(Action<JobBuilder> configureJob, Action<TriggerBuilder> configureTrigger) where TJob : IJob
        {
            var jobToBuild = JobBuilder.Create<TJob>();
            configureJob(jobToBuild);
            var job = jobToBuild.Build();

            var triggerToBuild = TriggerBuilder.Create();
            configureTrigger(triggerToBuild);
            var trigger = triggerToBuild.Build();

            quartzConfiguration.Scheduler.ScheduleJob(job, trigger);

            return Task.FromResult(0);
        }
    }
}
using System;
using System.Collections.Generic;
using System.Linq;

using Castle.Facilities.Logging;
using Castle.MicroKernel.Registration;
using Castle.Windsor.Installer;

using FluentAssemblyScanner;

using NLog;

using Quartz;
using Quartz.Impl;
using Quartz.Spi;

using QuartzCore.Dependency;
using QuartzCore.Quartz;

using AssemblyFilter = Castle.MicroKernel.Registration.AssemblyFilter;

namespace QuartzCore
{
    public class Program
    {
        public static List<Type> FindTypesBasedOn<T>()
        {
            return (from asm in typeof(T).FindAssemliesInBin()
                    from type in asm.GetTypes()
                    where type.IsClass && typeof(T).IsAssignableFrom(type)
                    select type)
                .ToList();
        }

        public static void Main(string[] args)
        {
            InstallCore();

            var scheduler = GetScheduler();

            InstallModules();

            InstallJobs(scheduler);

            scheduler.Start();
        }

        private static IScheduler GetScheduler()
        {
            var scheduler = StdSchedulerFactory.GetDefaultScheduler();

            scheduler.JobFactory = IocManager.Instance.Resolve<IJobFactory>();
            scheduler.ListenerManager.AddJobListener(IocManager.Instance.Resolve<IJobListener>());

            return scheduler;
        }

        private static void InstallCore()
        {
            IocManager.Instance.Container.Install(FromAssembly.This())
                       .AddFacility<LoggingFacility>(facility => facility.UseNLog("NLog.config"))
                       .Register(
                           Component.For<IJobFactory, QuartzWindsorFactory>().LifestyleSingleton(),
                           Component.For<IJobListener, JobListener>().LifestyleSingleton(),
                           Component.For<ILogger>().UsingFactoryMethod(() => LogManager.GetLogger("Debug")).LifestyleTransient(),
                           Classes.FromAssemblyInDirectory(new AssemblyFilter(string.Empty))
                                   .IncludeNonPublicTypes()
                                   .BasedOn<IPayflexJob>()
                                   .WithService.Self()
                                   .LifestyleTransient(),
                           Classes.FromAssemblyInDirectory(new AssemblyFilter(string.Empty))
                                   .IncludeNonPublicTypes()
                                   .BasedOn<JobModuleBase>()
                                   .WithService.Self()
                                   .LifestyleTransient()
                       );
        }

        private static void InstallJobs(IScheduler scheduler)
        {
            foreach (var job in FindTypesBasedOn<IPayflexJob>())
            {
                var jobToExecute = (JobBase)IocManager.Instance.Resolve(job);
                if (jobToExecute != null)
                {
                    jobToExecute.CreateJobDetail();
                    jobToExecute.CreateJobTrigger();
                    jobToExecute.Build();
                    jobToExecute.Validate();
                    scheduler.ScheduleJob(jobToExecute.JobDetail, jobToExecute.Trigger);
                }
            }
        }

        private static void InstallModules()
        {
            var jobModules = FluentAssemblyScanner.FluentAssemblyScanner
                .FromAssemblyInDirectory(AssemblyFilterFactory.All())
                .IncludeNonPublicTypes()
                .BasedOn<JobModuleBase>()
                .Filter()
                .Classes().NonStatic().Scan();

            foreach (var jobModule in jobModules)
            {
                var module = IocManager.Instance.Resolve<JobModuleBase>(jobModule);
                module.BeforeCreateJob();
                module.AfterCreateJob();
                IocManager.Instance.Release(module);
            }
        }
    }
}
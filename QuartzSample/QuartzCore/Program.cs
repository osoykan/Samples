using System.Linq;

using Castle.Facilities.Logging;
using Castle.MicroKernel.Registration;
using Castle.Windsor;
using Castle.Windsor.Installer;

using NLog;

using Quartz;
using Quartz.Impl;
using Quartz.Spi;

using QuartzCore.Dependency;
using QuartzCore.Quartz;

namespace QuartzCore
{
    public class Program
    {
        public static void Main(string[] args)
        {
            var container = new WindsorContainer();

            container.Install(FromAssembly.This())
                     .AddFacility<LoggingFacility>(facility => facility.UseNLog("NLog.config"))
                     .Register(
                         Component.For<IJobFactory, QuartzWindsorFactory>().LifestyleSingleton(),
                         Component.For<IJobListener, JobListener>().LifestyleSingleton(),
                         Component.For<ILogger>().UsingFactoryMethod(() => LogManager.GetLogger("Debug")).LifestyleTransient(),
                         Classes.FromAssemblyInDirectory(new AssemblyFilter(string.Empty))
                                .IncludeNonPublicTypes()
                                .BasedOn<IPayflexJob>()
                                .WithService.Self()
                                .LifestyleTransient()
                );

            var scheduler = StdSchedulerFactory.GetDefaultScheduler();
            scheduler.JobFactory = container.Resolve<IJobFactory>();
            scheduler.ListenerManager.AddJobListener(container.Resolve<IJobListener>());

            var jobs = (from asm in typeof(IPayflexJob).FindAssemliesInBin()
                        from type in asm.GetTypes()
                        where type.IsClass && typeof(IPayflexJob).IsAssignableFrom(type)
                        select type)
                .ToList();

            foreach (var job in jobs)
            {
                var jobToExecute = container.Resolve(job) as JobBase;
                if (jobToExecute != null)
                {
                    jobToExecute.CreateJobDetail();
                    jobToExecute.CreateJobTrigger();
                    jobToExecute.Build();
                    jobToExecute.Validate();
                    scheduler.ScheduleJob(jobToExecute.JobDetail, jobToExecute.Trigger);
                }
            }

            scheduler.Start();
        }
    }
}
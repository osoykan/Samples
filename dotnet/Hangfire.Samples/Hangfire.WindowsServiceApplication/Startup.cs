#region using

using System;
using System.Threading;

using Hangfire.SqlServer;
using Hangfire.WindowsServiceApplication;

using Microsoft.Owin;

using Owin;

#endregion

[assembly: OwinStartup(typeof(Startup))]

namespace Hangfire.WindowsServiceApplication
{
    public class Startup
    {
        public void Configuration(IAppBuilder app)
        {
            app.UseErrorPage();
            app.UseWelcomePage("/");

            GlobalConfiguration.Configuration
                               .UseSqlServerStorage("DefaultConnection",
                                   new SqlServerStorageOptions
                                   {
                                       QueuePollInterval = TimeSpan.FromSeconds(value: 1)
                                   });

            app.UseHangfireDashboard();
            app.UseHangfireServer();

            string command;
            while ((command = Console.ReadLine()) != string.Empty)
            {
                if ("job".Equals(command, StringComparison.OrdinalIgnoreCase))
                {
                    BackgroundJob.Enqueue(() => Console.WriteLine($"{Guid.NewGuid()} - BackgroundJob job completed successfully ({DateTime.Now})!"));
                }
            }

            //RecurringJob.AddOrUpdate(
            //    () => Console.WriteLine($"{DateTime.Now} Recurring job completed successfully!"),
            //    Cron.Minutely);
        }
    }
}
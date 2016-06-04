#region using

using System;

using Hangfire.ConsoleApplication;
using Hangfire.SqlServer;

using Microsoft.Owin;

using Owin;

#endregion

[assembly: OwinStartup(typeof(Startup))]

namespace Hangfire.ConsoleApplication
{
    public class Startup
    {
        public void Configuration(IAppBuilder app)
        {
            app.UseErrorPage();
            app.UseWelcomePage("/");

            GlobalConfiguration.Configuration.UseSqlServerStorage(
                "DefaultConnection",
                new SqlServerStorageOptions
                {
                    QueuePollInterval = TimeSpan.FromSeconds(value: 1)
                });

            app.UseHangfireDashboard();
            app.UseHangfireServer();

            //RecurringJob.AddOrUpdate(
            //    () => Console.WriteLine("{0} Recurring job completed successfully!", DateTime.Now),
            //    Cron.Minutely);
        }
    }
}
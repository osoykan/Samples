#region using

using System;
using System.IO;

using Hangfire.Logging;
using Hangfire.Logging.LogProviders;

using Microsoft.Owin.Hosting;

using Topshelf;

#endregion

namespace Hangfire.WindowsServiceApplication
{
    internal class Program
    {
        private const string Endpoint = "http://localhost:12345";

        private static void Main()
        {
            // Configure AppDomain parameter to simplify the config – http://stackoverflow.com/a/3501950/1317575
            // AppDomain.CurrentDomain.SetData("DataDirectory", Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "App_Data"));

            LogProvider.SetCurrentLogProvider(new ColouredConsoleLogProvider());

            HostFactory.Run(x =>
            {
                x.Service<Application>(s =>
                {
                    s.ConstructUsing(name => new Application());
                    s.WhenStarted(tc => tc.Start());
                    s.WhenStopped(tc => tc.Stop());
                });
                x.RunAsLocalSystem();

                x.SetDescription("Hangfire Windows Service Sample");
                x.SetDisplayName("Hangfire Windows Service Sample");
                x.SetServiceName("hangfire-sample");
            });
        }

        private class Application
        {
            private IDisposable host;

            public void Start()
            {
                host = WebApp.Start<Startup>(Endpoint);

                Console.WriteLine();
                Console.WriteLine("Hangfire Server started.");
                Console.WriteLine("Dashboard is available at {0}/hangfire", Endpoint);
                Console.WriteLine();
            }

            public void Stop()
            {
                host.Dispose();
            }
        }
    }
}
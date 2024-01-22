#region using

using System;
using System.IO;

using Hangfire.Logging;
using Hangfire.Logging.LogProviders;

using Microsoft.Owin.Hosting;

#endregion

namespace Hangfire.ConsoleApplication
{
    internal class Program
    {
        private static void Main()
        {
            // Configure AppDomain parameter to simplify the config – http://stackoverflow.com/a/3501950/1317575
            // AppDomain.CurrentDomain.SetData("DataDirectory", Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "App_Data"));

            LogProvider.SetCurrentLogProvider(new ColouredConsoleLogProvider());

            const string Endpoint = "http://localhost:12345";

            using (WebApp.Start<Startup>(Endpoint))
            {
                Console.WriteLine();
                Console.WriteLine($"{DateTime.Now} Hangfire Server started.");
                Console.WriteLine($"{DateTime.Now} Dashboard is available at {Endpoint}/hangfire");
                Console.WriteLine();
                Console.WriteLine($"{DateTime.Now} Type JOB to add a background job.");
                Console.WriteLine($"{DateTime.Now} Press ENTER to exit...");

                string command;
                while ((command = Console.ReadLine()) != string.Empty)
                {
                    if ("job".Equals(command, StringComparison.OrdinalIgnoreCase))
                    {
                        BackgroundJob.Enqueue(() => Console.WriteLine($"{Guid.NewGuid()} - BackgroundJob job completed successfully ({DateTime.Now})!"));
                    }
                }
            }
        }
    }
}
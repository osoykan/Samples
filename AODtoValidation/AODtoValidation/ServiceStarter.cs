namespace AODtoValidation
{
    #region using

    using System;
    using System.Reflection;
    using System.ServiceProcess;
    using System.Threading;

    #endregion

    public static class ServiceStarter
    {
        public static void CheckServiceRegisteration(string[] args)
        {
            if (args != null && args.Length > 0)
            {
                if (args[0].Equals("-i", StringComparison.OrdinalIgnoreCase))
                {
                    SelfInstaller.InstallMe();
                    Environment.Exit(0);
                }
                else if (args[0].Equals("-u", StringComparison.OrdinalIgnoreCase))
                {
                    SelfInstaller.UninstallMe();
                    Environment.Exit(0);
                }
            }
        }

        public static void StartApplication<T>(string[] args) where T : ServiceBase, new()
        {
            var servicesToRun = new ServiceBase[]
            {
                new T()
            };

            StartApplication(servicesToRun, args);
        }

        public static void StartApplication(ServiceBase[] services, string[] args)
        {
            //Check service registeration before go to
            CheckServiceRegisteration(args);

            if (Environment.UserInteractive)
                RunAsConsole(services);
            else
                ServiceBase.Run(services);
        }

        private static void RunAsConsole(ServiceBase[] servicesToRun)
        {
            Console.ForegroundColor = ConsoleColor.DarkGreen;
            Console.WriteLine("Services running in console mode.");
            Console.WriteLine();

            var onStartMethod = typeof (ServiceBase).GetMethod("OnStart",
                BindingFlags.Instance | BindingFlags.NonPublic);
            foreach (var service in servicesToRun)
            {
                Console.Write("Starting {0}...", service.ServiceName);
                onStartMethod.Invoke(service, new object[] {new string[] {}});
                Console.Write("Started");
            }

            Console.WriteLine();
            Console.WriteLine();
            Console.ForegroundColor = ConsoleColor.DarkRed;
            Console.WriteLine("Press ESC to stop the services and end the process...");
            while (Console.ReadKey().Key != ConsoleKey.Escape)
                Console.ReadKey();
            Console.WriteLine();
            Console.ForegroundColor = ConsoleColor.DarkGreen;
            var onStopMethod = typeof (ServiceBase).GetMethod("OnStop", BindingFlags.Instance | BindingFlags.NonPublic);

            foreach (var service in servicesToRun)
            {
                Console.Write("Stopping {0}...", service.ServiceName);
                onStopMethod.Invoke(service, null);
                Console.Write("Stopped {0}...", service.ServiceName);
            }

            Console.WriteLine("All services stopped.");
            Thread.Sleep(1000);
        }
    }
}
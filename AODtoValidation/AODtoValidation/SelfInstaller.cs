namespace AODtoValidation
{
    #region using

    using System.Configuration.Install;
    using System.Reflection;

    #endregion

    public static class SelfInstaller
    {
        private static readonly string ExePath = Assembly.GetExecutingAssembly().Location;

        public static bool InstallMe()
        {
            try
            {
                ManagedInstallerClass.InstallHelper(new[] {ExePath});
            }
            catch
            {
                return false;
            }
            return true;
        }

        public static bool UninstallMe()
        {
            try
            {
                ManagedInstallerClass.InstallHelper(new[] {"/u", ExePath});
            }
            catch
            {
                return false;
            }
            return true;
        }
    }
}
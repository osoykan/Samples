namespace AODtoValidation.Installers
{
    #region using

    using Castle.Facilities.WcfIntegration;
    using Castle.MicroKernel.Registration;
    using Castle.MicroKernel.SubSystems.Configuration;
    using Castle.Windsor;
    using Interceptor;

    #endregion

    public class InterceptorInstaller : IWindsorInstaller
    {
        public void Install(IWindsorContainer container, IConfigurationStore store)
        {
            container.Register(
                Component.For<ValidatorInterceptor>().LifestylePerWcfOperation()
                );
        }
    }
}
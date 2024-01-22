namespace AODtoValidation.Installers
{
    #region using

    using System.ServiceModel;
    using Castle.Core;
    using Castle.Facilities.WcfIntegration;
    using Castle.MicroKernel.Registration;
    using Castle.MicroKernel.SubSystems.Configuration;
    using Castle.Windsor;
    using Interceptor;
    using Service;

    #endregion

    public class MobileServiceInstaller : IWindsorInstaller
    {
        public void Install(IWindsorContainer container, IConfigurationStore store)
        {
            var url = "http://127.0.0.1/MobileService";
            container.Register(
                Component.For<IMobileService>().ImplementedBy<MobileService>()
                .Interceptors(InterceptorReference.ForType<ValidatorInterceptor>()).First
                    .AsWcfService(new DefaultServiceModel().AddEndpoints(
                        WcfEndpoint.BoundTo(new BasicHttpBinding
                        {
                            Security = new BasicHttpSecurity
                            {
                                Mode = BasicHttpSecurityMode.None,
                                Transport = new HttpTransportSecurity
                                {
                                    ClientCredentialType = HttpClientCredentialType.None
                                }
                            }
                        }).At(url)).AddBaseAddresses(url).PublishMetadata(o => o.EnableHttpGet())
                    ));
        }
    }
}
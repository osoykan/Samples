namespace AODtoValidation
{
    #region using

    using System;
    using System.ServiceModel.Description;
    using Castle.Facilities.WcfIntegration;
    using Castle.MicroKernel.Registration;
    using Castle.Windsor;
    using Castle.Windsor.Installer;

    #endregion

    public class Bootstrapper
    {
        public IWindsorContainer Container { get; private set; }

        public void Dispose()
        {
            Container.Dispose();
        }

        public void Start()
        {
            var metadata = new ServiceMetadataBehavior
            {
                HttpGetEnabled = true,
                HttpsGetEnabled = true
            };
            var returnFaults = new ServiceDebugBehavior {IncludeExceptionDetailInFaults = true};

            Container = new WindsorContainer()
                .AddFacility<WcfFacility>(f => f.CloseTimeout = TimeSpan.Zero)
                .Install(FromAssembly.This())
                .Register(
                    Component.For<IEndpointBehavior>().ImplementedBy<WebHttpBehavior>(),
                    Component.For<IServiceBehavior>().Instance(metadata),
                    Component.For<IServiceBehavior>().Instance(returnFaults));
        }
    }
}
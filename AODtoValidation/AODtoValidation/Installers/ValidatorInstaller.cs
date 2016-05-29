namespace AODtoValidation.Installers
{
    #region using

    using Castle.MicroKernel.Registration;
    using Castle.MicroKernel.SubSystems.Configuration;
    using Castle.Windsor;
    using FluentValidation;
    using Validators;

    #endregion

    public class ValidatorInstaller : IWindsorInstaller
    {
        public void Install(IWindsorContainer container, IConfigurationStore store)
        {
            container.Register(
                Classes.FromAssemblyContaining(typeof (ValidatorBase<>))
                    .IncludeNonPublicTypes()
                    .BasedOn(typeof (IValidator<>))
                    .WithServiceAllInterfaces()
                    .LifestyleTransient()
                );
        }
    }
}
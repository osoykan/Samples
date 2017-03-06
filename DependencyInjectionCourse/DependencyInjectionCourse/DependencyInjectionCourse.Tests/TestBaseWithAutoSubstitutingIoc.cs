using System;

using Autofac;
using Autofac.Features.ResolveAnything;

using DependencyInjectionCourse.Tests.Conventions;

namespace DependencyInjectionCourse.Tests
{
    public abstract class TestBaseWithAutoSubstitutingIoc
    {
        protected ContainerBuilder Builder;
        protected IContainer Resolver;

        protected TestBaseWithAutoSubstitutingIoc()
        {
            Builder = new ContainerBuilder();
            Builder.RegisterSource(new AnyConcreteTypeNotAlreadyRegisteredSource());
            Builder.RegisterSource(new AutoNSubstitutingRegistrationSource());
        }

        protected void Building(Action<ContainerBuilder> builderAction)
        {
            builderAction(Builder);
            Resolver = Builder.Build();
        }

        protected T AFake<T>()
        {
            return Resolver.Resolve<T>();
        }

        protected T The<T>()
        {
            return Resolver.Resolve<T>();
        }
    }
}

using System;

using Autofac;

namespace DependencyInjectionCourse.Tests
{
    public abstract class TestBaseWithLocalIoc
    {
        protected ContainerBuilder Builder;
        protected IContainer Resolver;

        protected TestBaseWithLocalIoc()
        {
            Builder = new ContainerBuilder();
        }

        protected TestBaseWithLocalIoc Building(Action<ContainerBuilder> builderAction)
        {
            builderAction(Builder);
            return this;
        }

        public IContainer Ok()
        {
            Resolver = Builder.Build();
            return Resolver;
        }
    }
}

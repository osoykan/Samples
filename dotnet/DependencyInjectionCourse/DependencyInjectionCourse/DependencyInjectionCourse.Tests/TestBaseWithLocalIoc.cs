﻿using System;

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

        protected void Building(Action<ContainerBuilder> builderAction)
        {
            builderAction(Builder);
            Resolver = Builder.Build();
        }

        protected T The<T>()
        {
            return Resolver.Resolve<T>();
        }
    }
}

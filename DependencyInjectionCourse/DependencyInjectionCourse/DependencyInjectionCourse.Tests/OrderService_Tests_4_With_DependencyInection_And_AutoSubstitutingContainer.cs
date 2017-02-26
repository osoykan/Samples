using Autofac;

using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;
using DependencyInjectionCourse.Tests.Conventions;

using FluentAssertions;

using NSubstitute;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    /// <summary>
    ///     Auto-Mocking container aspect <see cref="AutoNSubstitutingRegistrationSource" />
    /// </summary>
    /// <seealso cref="DependencyInjectionCourse.Tests.TestBaseWithAutoSubstitutingIoc" />
    public class OrderService_Tests_With_DependencyInection_And_AutoSubstitutingContainer : TestBaseWithAutoSubstitutingIoc
    {
        [Fact]
        public void with_dependency_injection_and_automocking_container()
        {
            //-----------------------------------------------------------------------------------------------------------
            // Arrange
            //-----------------------------------------------------------------------------------------------------------
            Building(builder => { builder.RegisterType<OrderService>().As<IOrderService>(); });

            AFake<ICacheManager>().Get("1").Returns(new Basket(1, 50));

            var sut = Use<IOrderService>();

            //-----------------------------------------------------------------------------------------------------------
            // Act
            //-----------------------------------------------------------------------------------------------------------
            OrderResult result = sut.DoOrder(1);

            //-----------------------------------------------------------------------------------------------------------
            // Assert
            //-----------------------------------------------------------------------------------------------------------
            result.BasketId.Should().Be(1);
            result.Total.Should().Be(50);
            AFake<IDependency1>().Received().Salute();
        }
    }
}

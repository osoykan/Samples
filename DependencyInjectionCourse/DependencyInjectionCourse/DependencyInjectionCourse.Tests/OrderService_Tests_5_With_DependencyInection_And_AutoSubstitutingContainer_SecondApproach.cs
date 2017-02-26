using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;

using FluentAssertions;

using NSubstitute;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    /// <summary>
    ///  Uses https://github.com/MRCollective/AutofacContrib.NSubstitute
    /// </summary>
    /// <seealso cref="DependencyInjectionCourse.Tests.TestBaseWithAutoSubstituteIoc" />
    public class OrderService_Tests_5_With_DependencyInection_And_AutoSubstitutingContainer_SecondApproach : TestBaseWithAutoSubstituteIoc
    {
        [Fact]
        public void with_dependency_injection_and_automocking_container()
        {
            //-----------------------------------------------------------------------------------------------------------
            // Arrange
            //-----------------------------------------------------------------------------------------------------------
            AFake<ICacheManager>().Get("1").Returns(new Basket(1, 50));
            IOrderService sut = Use<IOrderService, OrderService>();

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

using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;

using FakeItEasy;

using FluentAssertions;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    /// <summary>
    ///     Auto-Mocking container aspect <see cref="TestBaseWithAutoFakingIoc" />
    /// </summary>
    /// <seealso cref="DependencyInjectionCourse.Tests.TestBaseWithAutoFakingIoc" />
    public class OrderService_Tests_4_With_DependencyInection_And_AutoSubstitutingContainer : TestBaseWithAutoFakingIoc
    {
        [Fact]
        public void with_dependency_injection_and_automocking_container()
        {
            //-----------------------------------------------------------------------------------------------------------
            // Arrange
            //-----------------------------------------------------------------------------------------------------------
            A.CallTo(() => Fake<ICacheManager>().Get("1")).Returns(new Basket(1, 50));

            IOrderService sut = The<IOrderService, OrderService>();

            //-----------------------------------------------------------------------------------------------------------
            // Act
            //-----------------------------------------------------------------------------------------------------------
            OrderResult result = sut.DoOrder(1);

            //-----------------------------------------------------------------------------------------------------------
            // Assert
            //-----------------------------------------------------------------------------------------------------------
            result.BasketId.Should().Be(1);
            result.Total.Should().Be(50);
            A.CallTo(() => Fake<IDependency1>().Salute()).MustHaveHappened();
        }
    }
}

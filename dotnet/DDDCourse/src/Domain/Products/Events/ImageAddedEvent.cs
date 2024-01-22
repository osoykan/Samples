using Core.Bus;

namespace Products.Events
{
    public class ImageAddedEvent : Event
    {
        public int Order;
        public Product Product;
        public string Url;

        public ImageAddedEvent(Product product, string url, int order)
        {
            Order = order;
            Url = url;
            Product = product;
        }
    }
}

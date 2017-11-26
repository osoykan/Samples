using System.Threading.Tasks;

namespace Products.Services
{
    public interface IProductDomainService
    {
        Task Create(string name, string code, string barcode);
    }
}

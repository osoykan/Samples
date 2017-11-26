using System.Collections.Generic;
using System.Threading.Tasks;

using Core.ObjectMapper;
using Core.Repositories;

using Products.Dtos;

namespace Products.ReadModel
{
    public class ProductReadModel : IProductReadModel
    {
        private readonly IObjectMapper _mapper;
        private readonly IRepository<Product> _repository;

        public ProductReadModel(IRepository<Product> repository, IObjectMapper mapper)
        {
            _repository = repository;
            _mapper = mapper;
        }

        public async Task<List<ProductDto>> GetAll()
        {
            List<Product> products = await _repository.GetAll();

            return _mapper.MapTo<List<ProductDto>>(products);
        }

        public async Task<ProductDto> GetByBarcode(string barcode)
        {
            Product product = await _repository.Get(x => x.Barcode == barcode);

            return _mapper.MapTo<ProductDto>(product);
        }
    }
}

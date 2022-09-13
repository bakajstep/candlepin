require 'spec_helper'
require 'candlepin_scenarios'
require 'json'

describe 'Import Test Group:', :serial => true do

  include CandlepinMethods
  include VirtHelper

  RSpec.shared_examples "importer" do |async|

    before(:all) do
      # Decide the import method to use, async or synchronous
      if async
        @import_method = import_and_wait
      else
        @import_method = import_now
      end

      @cp = Candlepin.new('admin', 'admin')
      skip("candlepin running in hosted mode") if is_hosted?

      @cp_export = async ? AsyncStandardExporter.new : StandardExporter.new
      @cp_export.create_candlepin_export()
      @cp_export_file = @cp_export.export_filename
      @cp_correlation_id = "a7b79f6d-63ca-40d8-8bfb-f255041f4e3a"

      @candlepin_consumer = @cp_export.candlepin_client.get_consumer()
      @candlepin_consumer.unregister @candlepin_consumer['uuid']

      @import_owner = @cp.create_owner(random_string("test_owner"))
      @import_username = random_string("import-user")
      @import_owner_client = user_client(@import_owner, @import_username)
      import_record = @import_method.call(@import_owner['key'], @cp_export_file)
      import_record.status.should == 'SUCCESS'
      import_record.statusMessage.should == "#{@import_owner['key']} file imported successfully."
      @exporters = [@cp_export]
    end

    after(:all) do
      @cp.delete_user(@import_username) if @import_username
      @cp.delete_owner(@import_owner['key']) if @import_owner

      if @exporters
        @exporters.each do |e|
          e.cleanup()
        end
      end
    end

    def import_now
      lambda { |owner_key, export_file, param_map={}|
        @cp.import(owner_key, export_file, param_map)
      }
    end

    def import_and_wait
      lambda { |owner_key, export_file, param_map={}|
        headers = { :correlation_id => @cp_correlation_id }
        job = @cp.import_async(owner_key, export_file, param_map, headers)
        # Wait a little longer here as import can take a bit of time
        wait_for_job(job["id"], 10)
        status = @cp.get_job(job["id"], true)
        if status["state"] == "FAILED"
          raise AsyncImportFailure.new(status)
        end
        status["resultData"]
      }
    end

    # TODO
    it 'should allow forcing the same manifest' do
      # This test must run after a successful import has already occurred.
      @import_method.call(@import_owner['key'], @cp_export_file,
        {:force => ["MANIFEST_SAME", "DISTRIBUTOR_CONFLICT"]})
    end

    # TODO
    it 'should allow importing older manifests into another owner' do
      old_exporter = StandardExporter.new
      @exporters << old_exporter
      older = old_exporter.create_candlepin_export().export_filename

      new_exporter = StandardExporter.new
      @exporters << new_exporter
      newer = new_exporter.create_candlepin_export().export_filename

      owner1 = create_owner(random_string("owner1"))
      owner2 = create_owner(random_string("owner2"))
      @import_method.call(owner1['key'], newer)
      @import_method.call(owner2['key'], older)
    end

    # TODO
    it 'should return 409 when importing manifest from different subscription management application' do
      exporter = StandardExporter.new
      @exporters << exporter
      another = exporter.create_candlepin_export().export_filename

      old_upstream_uuid = @cp.get_owner(@import_owner['key'])['upstreamConsumer']['uuid']
      expected = "Owner has already imported from another subscription management application."
      exception = false;
      begin
        @import_method.call(@import_owner['key'], another)
      rescue RestClient::Exception => e
        async.should be false
        e.http_code.should == 409
        json = JSON.parse(e.http_body)
        exception = true
      rescue AsyncImportFailure => aif
        async.should be true
        json = aif.data["resultData"]
        json.should_not be_nil
        exception = true
      end

      exception.should be true
      if async
        json.include?(expected).should be true
        json.include?("DISTRIBUTOR_CONFLICT").should be true
      else
        json["displayMessage"].include?(expected).should be true
        json["conflicts"].size.should == 1
        json["conflicts"].include?("DISTRIBUTOR_CONFLICT").should be true
      end

      @cp.get_owner(@import_owner['key'])['upstreamConsumer']['uuid'].should == old_upstream_uuid

      exception = false
      # Try again and make sure we don't see MANIFEST_SAME appear: (this was a bug)
      begin
        @import_method.call(@import_owner['key'], another)
      rescue RestClient::Exception => e
        async.should be false
        e.http_code.should == 409
        json = JSON.parse(e.http_body)
        exception = true
      rescue AsyncImportFailure => aif
        async.should be true
        json = aif.data["resultData"]
        json.should_not be_nil
        exception = true
      end

      exception.should be true
      if async
        json.include?(expected).should be true
        json.include?("DISTRIBUTOR_CONFLICT").should be true
      else
        json["displayMessage"].include?(expected).should be true
        json["conflicts"].size.should == 1
        json["conflicts"].include?("DISTRIBUTOR_CONFLICT").should be true
      end
    end

    # TODO
    it 'should allow forcing a manifest from a different subscription management application' do
      exporter = StandardExporter.new
      @exporters << exporter
      another = exporter.create_candlepin_export().export_filename

      old_upstream_uuid = @cp.get_owner(@import_owner['key'])['upstreamConsumer']['uuid']
      pools = @cp.list_owner_pools(@import_owner['key'])
      pool_ids = pools.collect { |p| p['id'] }
      @import_method.call(@import_owner['key'], another,
        {:force => ['DISTRIBUTOR_CONFLICT']})
      @cp.get_owner(@import_owner['key'])['upstreamConsumer']['uuid'].should_not == old_upstream_uuid
      pools = @cp.list_owner_pools(@import_owner['key'])
      new_pool_ids = pools.collect { |p| p['id'] }
      # compare without considering order, pools should have changed completely:
      new_pool_ids.should_not =~ pool_ids
    end

    # TODO
    it 'should return 400 when importing manifest in use by another owner' do
      # Because the previous tests put the original import into a different state
      # than if you just run this single one, we need to clear first and then
      # re-import the original.
      # Also added the confirmation that the exception occurs when importing to
      # another owner.
      job = @import_owner_client.undo_import(@import_owner['key'])
      wait_for_job(job['id'], 30)

      @import_method.call(@import_owner['key'], @cp_export_file)
      owner2 = @cp.create_owner(random_string("owner2"))
      exception = false
      expected = "This subscription management application has already been imported by another owner."
      begin
        @import_method.call(owner2['key'], @cp_export_file)
      rescue RestClient::Exception => e
        async.should be false
        e.http_code.should == 400
        json = JSON.parse(e.http_body)
        exception = true
      rescue AsyncImportFailure => aif
        async.should be true
        json = aif.data["resultData"]
        json.should_not be_nil
        exception = true
      end

      @cp.delete_owner(owner2['key'])
      exception.should be true
      if async
        json.include?(expected).should be true
      else
        message = json["displayMessage"]
        message.should_not be_nil
        message.should == expected
      end
    end


    #### TODO
    it 'contains upstream consumer' do
      # this information used to be on /imports but now exists on Owner
      # checking for api and webapp overrides
      consumer = @candlepin_consumer

      upstream = @cp.get_owner(@import_owner['key'])['upstreamConsumer']
      upstream.uuid.should == consumer['uuid']
      upstream.apiUrl.should == "api1"
      upstream.webUrl.should == "webapp1"
      upstream.id.should_not be_nil
      upstream.idCert.should_not be_nil
      upstream.name.should == consumer['name']

      # Delete the created and updated fields, as the DTO does not contain these fields
      upstream['type'].delete('created');
      upstream['type'].delete('updated');
      consumer['type'].delete('created');
      consumer['type'].delete('updated');

      # upstream.type caused a failure on some machines
      upstream['type'].should == consumer['type']
    end

    it 'should contain all derived product data' do
      pool = @cp.list_pools(:owner => @import_owner.id,
        :product => @cp_export.products[:product3].id)[0]
      pool.should_not be_nil
      pool["derivedProductId"].should == @cp_export.products[:derived_product].id
      pool["derivedProvidedProducts"].length.should == 1
      pool["derivedProvidedProducts"][0]["productId"].should == @cp_export.products[:derived_provided_prod].id
    end

    it 'should contain branding info' do
      pool = @cp.list_pools(:owner => @import_owner.id,
        :product => @cp_export.products[:product1].id)[0]
      pool['branding'].length.should == 1
      pool['branding'][0]['productId'].should == @cp_export.products[:eng_product]['id']
      pool['branding'][0]['name'].should == "Branded Eng Product"
    end

    it 'should put the cdn from the manifest into the created subscriptions' do
      @cp.list_subscriptions(@import_owner['key']).find_all do |sub|
          sub['cdn']['label'].should == @cp_export.cdn_label
      end
    end

  end

  describe "Async Tests" do
    it_should_behave_like "importer", true
  end

  describe "Sync import tests" do
    it_should_behave_like "importer", false
  end

end

class AsyncImportFailure < Exception
  attr_reader :data

  def initialize(data)
    @data = data
  end

end

